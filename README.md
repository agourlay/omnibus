Omnibus [![Build Status](https://travis-ci.org/agourlay/omnibus.png?branch=master)](https://travis-ci.org/agourlay/omnibus)
=========

Omnibus is an HTTP-friendly reactive message bus which means :

 - Topic hierachies and subscriptions are managed via a rest API.
 - Updates are streamed by [Server-Sent-Event](http://www.html5rocks.com/en/tutorials/eventsource/basics/) which can be easily consumed by javascript frontends. 
 - With reactive modes it is possible to replay specific parts of the events.
 - Subscriptions can be composed via the url keyword `+`.
 - It can be integrated in an existing [Akka](http://akka.io/) application. (experimental)

**This is still a work in progress, any API is likely to change** 
 
## REST & hal+json

Omnibus follows the specification [hal+json](http://stateless.co/hal_specification.html) to expose its resources. 

It simply means that the REST API is easily discoverable.

Let's demonstrate how it works with some basic commands using CURL.

Topics are trees, you can create them simply with a POST request.

The root url of every topic is `/topics`, this keyword is reserved.

> curl -X GET http://localhost:8080/topics/

```json
[]
```

We receive the empty collection of roots.

Use POST to create the nested topic "/topics/animals/furry".

> curl -X POST http://localhost:8080/topics/animals/furry

We can now retrieve informations about the topic we just created using GET

> curl -X GET http://localhost:8080/topics/animals

```json
{
  "topic": ["animals"],
  "subTopicsNumber": 1,
  "subscribersNumber": 0,
  "eventsNumber": 0,
  "viewDate": 1390212364,
  "_embedded": {
    "children": [{
      "furry": {
        "href": "/topics/animals/furry"
      }
    }]
  },
  "_links": [{
    "self": {
      "href": "/topics/animals"
    }
  }, {
    "subscribe": {
      "href": "/stream/topics/animals"
    }
  }, {
    "stats": {
      "href": "/stats/topics/animals"
    }
  }]
}
``` 

You get there almost all the informations you need to interact with a topic in a REST fashion way.

With PUT you can push data to an existing topic.

> curl -X PUT http://localhost:8080/topics/animals -d "dolphins are the best"

If you publish a message at the `/animals` level, all subtopics will receive it as well.

It is possible to DELETE a topic and all its subtopics via the [administration](https://github.com/agourlay/omnibus#administration) API. 

And finally you can subscribe to the notifications on a topic.

> curl -X GET http://localhost:8080/stream/topics/animals

> ~~> Streaming subscription for topics /animals

When you subscribe to a topic, you will of course receive all the notifications targetting its sub topics.


## Reactive modes

Omnibus supports reactive modes in order to replay specific sequence of events from topics.

The supported modes are: 

- `simple`   : classic subscription (default one if not specified)

- `last`     : get last message on a topic and the following events
  - e.g http://localhost:8080/topics/stock/nasdaq?mode=last

- `replay`   : get all past messages on topic and the following events
  - e.g http://localhost:8080/topics/customer/order?mode=replay

- `since-id` : all the past events since a given event-id and the following events
  - e.g http://localhost:8080/topics/worldcup?mode=since-id&since=120

- `since-ts` : all the past events since a given unix timestamp and the following events
  - e.g  http://localhost:8080/topics/logs?mode=since-ts&since=1388250283

- `between-id` : all the events between two given event-id 
  - e.g http://localhost:8080/topics/worldcup?mode=between-id&since=12&to=200

- `between-ts` : all the events between two given unix timestamp
  - e.g  http://localhost:8080/topics/logs?mode=between-ts&since=1388250283&to=1388250552

Modes are specified by url parameter
> curl -X GET "http://localhost:8080/topics/results/basketball?mode=between-id&since=1&to=2"

> ~~> Streaming subscription for topics /results/basketball with mode replay

> id: 1
> event: result/basketball
> data: A basket ball game result
> timestamp: 1388250283

> id: 2
> event: result/basketball
> data: Another basket ball game result
> timestamp: 1388250552

## Composable subscriptions

You can compose subscriptions with the char `+` in order to merge notifications from multiple topics.

> curl -X GET http://localhost:8080/topics/customer/order/+/logistic/export

> ~~> Streaming subscription for topics /customer/order + /logistic/export with mode simple

All the topics must exist at the moment of the subscription or the whole request will be rejected.

Of course you are can use reactive modes on composed subscriptions but be ready to handle the flow of data if you target a root topic with the replay mode :D

## Administration

All administration features are protected by http basic authentication. (better than nothing for now)

By default the admin credentials are `admin/omnibus`, this can be changed it the configuration file.

The administration module exposes two APIs

- `DELETE /admin/topics/{topic-name}` to delete a topic and its subtopics
- `GET /admin/leaves` to retrieve all the topic leaves

You can also access the administration web interface running on http://localhost:8080/. (still a work in progress)

## Monitoring

If you just want to get raw data about usage, omnibus exposes statistics concerning all topics and the system itself following three modes.

- `live` : get the current statistics. (default mode)
  - e.g  http://localhost:8080/stats/topics/animals/furry/
  - e.g  http://localhost:8080/stats/system
- `history` : get all statistics history available. (you can configure retention time)
  - e.g  http://localhost:8080/stats/topics/animals/furry/?mode=history
  - e.g  http://localhost:8080/stats/system?mode=history (not yet available)
- `streaming` : continous data stream of statistics in realtime
  - e.g  http://localhost:8080/stats/topics/animals/furry?mode=streaming
  - e.g  http://localhost:8080/stats/topics/system?mode=streaming

## Persistence

Omnibus persists events in order to be able to replay them later.

By default those are persisted to disk but it is possible to store events using an external database.

So far only Cassandra is supported through the akka-persistence-plugin, see [here](https://github.com/krasserm/akka-persistence-cassandra#configuration) for configuration.

The retention time is configurable aswell for persistence solution.

## Usage and installation

There are two ways of running Omnibus

### As a standalone process.

Get the latest omnibus.tar distribution, extract and run the starting script in `/bin`.

This starts Omnibus on default port 8080.

You can configure the system by changing the properties in `/conf/application.conf`.

### As an embedded library (experimental)

It is possible to integrate Omnibus in an existing Akka application.

Add the latest omnibus.jar to your application by building from source with sbt `publishLocal`

```
libraryDependencies += "com.agourlay" % "omnibus" % "0.1-SNAPSHOT"
```

and then call :

```scala
val receptionist : OmnibusReceptionist = omnibus.service.OmnibusBuilder.start()
``` 

This will start the Omnibus system on the given port and return an OmnibusReceptionist object that offers all the useful methods to interact with the system :

```scala
  def createTopic(topicName : String)
  def deleteTopic(topicName : String)
  def checkTopic(topicName : String) : Future[Boolean]
  def publishToTopic(topicName : String, message : String) 
  def subToTopic(topicName : String, subscriber : ActorRef, mode : ReactiveCmd) : Future[Boolean]
  def unsubFromTopic(topicName : String, subscriber : ActorRef)
  def shutDownOmnibus()
```  

The subscriber's actorRef will receive this kind of updates

```scala
omnibus.domain.Message(id : Long, topicName : String, payload : String, timestamp : Long)
```