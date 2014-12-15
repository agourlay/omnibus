Omnibus  [![Build Status](https://travis-ci.org/agourlay/omnibus.png?branch=master)](https://travis-ci.org/agourlay/omnibus)
=========

Omnibus is an HTTP-friendly persistent message bus which means :

- Topic hierarchies and subscriptions are managed via a rest API.
- Updates are streamed by WebSocket or Server-Sent-Event.
- With reactive modes it is possible to replay specific parts of the events.
- Subscriptions can be composed via the URL keyword +.
- Most of the features can be accessed via the administration web interface.
- Events are persisted on the file system.

## Status

Omnibus is in **alpha** stage. It is under active development. Breaking changes could occur at any moment.

Still reading after this disclaimer? Let's start!

## Getting started

Get the latest [omnibus.tar](https://github.com/agourlay/omnibus/releases) distribution, extract and run the starting script in `/bin`.

This starts Omnibus on default port 8080 for HTTP and port 8081 for WebSockets.

You can configure the system by changing the properties in `/conf/application.conf`.

```
omnibus {
    http {
        port = 8080
    }
    websocket {
        port = 8081
        enable = true
    }
    admin {
        userName = "admin"
        password = "omnibus"
    }
    topic {
        retentionTime = "3 days"
    }
    graphite {
        enable = false
        host = "graphite.example.com"
        port = 2003
        prefix = "omnibus"
    }
}
```

The system is running, now let's explore the API.
 
## REST & hal+json

Omnibus follows the specification [hal+json](http://stateless.co/hal_specification.html) to expose its resources. 

It simply means that the REST API is easily discoverable.

Let's demonstrate how it works with some basic commands using CURL.

Topics are trees, you can create them simply with a POST request.

The root URL of every topic is `/topics`.

> curl -X GET http://localhost:8080/topics

```json
[]
```

We receive the empty collection of roots.

Use POST to create the nested topic `/topics/animals/furry`.

> curl -X POST http://localhost:8080/topics/animals/furry

We can now retrieve informations about the topic we just created using GET

> curl -X GET http://localhost:8080/topics/animals

```json
{
  "topic": ["animals"],
  "subTopicsNumber": 1,
  "subscribersNumber": 0,
  "eventsNumber": 0,
  "creationDate" : 1390212312,
  "viewDate": 1390212364,
  "_embedded": {
    "children": [
      { "furry": { "href": "/topics/animals/furry" }}
    ]
  },
  "_links": [
    { "self": { "href": "/topics/animals" }},
    { "subscribe": { "href": "/streams/topics/animals" }},
    { "stats": { "href": "/stats/topics/animals" }}
  ]
}
``` 

You get there almost all the information you need to interact with a topic in a REST fashion way.

With PUT you can push data to an existing topic.

> curl -X PUT http://localhost:8080/topics/animals -d "dolphins are the best"

If you publish a message at the `/animals` level, all subtopics will receive it as well.

It is possible to DELETE a topic and all its subtopics via the [administration](https://github.com/agourlay/omnibus#administration) API. 

You can also request the collection of leaves topic using `GET /leaves`. It is a streamed API, so you will receive topics as the topic trees are traversed. 

> curl -X GET http://localhost:8080/leaves

> ~~> Streaming topic view 

And finally you can of course subscribe to the notifications on a topic via WebSockets or SSE.
The URL are identical except the port and the protocol : 
> ws://localhost:8081/streams/topics/animals
or
> http://localhost:8080/streams/topics/animals


## Subscription models

Omnibus follows the common subscription model used in similar systems :

- when subscribing to a `parent` topic, one will see all updates occurring in the subtopics as well.
- when subscribing to a `leaf` topic, one will only see the updates targeting this topic directly.

## Reactive modes

Omnibus supports reactive modes via URL parameters in order to replay specific sequence of events from topics.

The supported modes are: 

- `simple`   : classic subscription (default one if not specified)

- `replay`   : get all past messages on topic and the following events
  - e.g http://localhost:8080/streams/topics/customer/order?react=replay
  - e.g ws://localhost:8081/streams/topics/customer/order?react=replay

- `since-id` : all the past events since a given event-id and the following events
  - e.g http://localhost:8080/streams/topics/worldcup?react=since-id&since=120
  - e.g ws://localhost:8081/streams/topics/worldcup?react=since-id&since=120

- `since-ts` : all the past events since a given unix timestamp and the following events
  - e.g  http://localhost:8080/streams/topics/logs?react=since-ts&since=1388250283
  - e.g  ws://localhost:8081/streams/topics/logs?react=since-ts&since=1388250283

- `between-id` : all the events between two given event-id 
  - e.g http://localhost:8080/streams/topics/worldcup?react=between-id&since=12&to=200
  - e.g ws://localhost:8081/streams/topics/worldcup?react=between-id&since=12&to=200

- `between-ts` : all the events between two given unix timestamp
  - e.g  http://localhost:8080/streams/topics/logs?react=between-ts&since=1388250283&to=1388250552
  - e.g  ws://localhost:8081/streams/topics/logs?react=between-ts&since=1388250283&to=1388250552

> curl -X GET "http://localhost:8080/streams/topics/results/basketball?react=between-id&since=1&to=2"

> ~~> Streaming subscription for topics /results/basketball with mode replay

> id: 1
> event: /result/basketball
> data: A basket ball game result
> timestamp: 1388250283

> id: 2
> event: /result/basketball
> data: Another basket ball game result
> timestamp: 1388250552

## Composable subscriptions

You can compose subscriptions with the char `+` in order to merge notifications from multiple topics.

> curl -X GET http://localhost:8080/streams/topics/customer/order/+/logistic/export

> ~~> Streaming subscription for topics /customer/order + /logistic/export with mode simple

All the topics must exist at the moment of the subscription or the whole request will be rejected.

Of course you are can use reactive modes on composed subscriptions but be ready to handle the flow of data if you target a root topic with the replay mode.

## Persistence

Omnibus persists the events on the local file-system in order to be able to replay them later if needed. 

You can find the your data in the 'data' folder, you can delete the content manually for a rapid maintenance clean up.

## Administration

All administration features are protected by HTTP basic authentication. (better than nothing for now)

By default the admin credentials are `admin/omnibus`, this can be changed in the configuration file.

The administration module exposes 3 API :

- `DELETE /admin/topics/{topic-name}` to delete a topic and its subtopics

- `GET /admin/subscribers` get all subscriptions

- `DELETE /admin/subscribers/{subscriber-id}` kill a subscription

You can also use the administration web interface running on http://localhost:8080/ to manually access most of the API and statistics.

![screenshot](https://github.com/agourlay/omnibus/raw/master/docs/screenshot.png)

And here some real-time stats about a topic

![screenshot](https://github.com/agourlay/omnibus/raw/master/docs/load.png)

## Monitoring

Omnibus can be configured to report its usage statistics to a Graphite instance.

```
graphite {
  enable = false
  host = "graphite.example.com:2003"
}
```

If you don't want to use Graphite, you can query manually statistics concerning all topics following two modes.

- `live` : get the current statistics. (default mode)
  - e.g  http://localhost:8080/stats/topics/animals/furry/

- `streaming` : continuous data stream of statistics in real-time
  - e.g  http://localhost:8080/stats/topics/animals/furry?mode=streaming

## Build and contribute

Build the project with SBT, I personally like `~re-start` to restart the application automatically when a file system change occurs.

If you want to build the frontend, you need [Bower](http://bower.io/) and [Grunt](http://gruntjs.com/) for Javascript build management.

Then in the folder `src/main/resources/frontend` run the following commands

>npm install

>bower install

>grunt

Then you are good to go!