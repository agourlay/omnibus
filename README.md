Omnibus [![Build Status](https://travis-ci.org/agourlay/omnibus.png?branch=master)](https://travis-ci.org/agourlay/omnibus)
=========

Omnibus is an HTTP-friendly reactive message bus which means :

 - Topic hierachies and subscriptions are managed via a rest API.
 - Updates are streamed by [Server-Sent-Event](http://www.html5rocks.com/en/tutorials/eventsource/basics/) which can be easily consumed by javascript frontends. 
 - With reactive modes it is possible to replay specific parts of the events.
 - Subscriptions can be composed via the url keyword `+`.
 - It can be easily integrated in an existing [Akka](http://akka.io/) application.

**This is still a work in progress, any API is likely to change** 
 
## Classic bus features

Let's demonstrate some basic commands using Curl.

Topics are trees, you can create them simply with a POST request.

The root url of every topic is "/topics", this keyword is reserved.

To create the nested topic "/topics/animals/furry" and directly push data on the new topic.

> curl -X POST http://localhost:8080/topics/animals/furry -d "cats are the best"

POST can be used without message to create an empty topic.

With PUT it is only possible to push data to an existing topic.

> curl -X PUT http://localhost:8080/topics/animals -d "dolphins are the best"

If you publish a message at the "/animals" level, all subtopics will receive it as well.

And finally you can subscribe to the notifications on a topic.

> curl -X GET http://localhost:8080/topics/animals

> ~~> Streaming subscription for topics /animals

When you subscribe to a topic, you will of course receive all the notifications targetting its sub topics.

It will be soon possible to DELETE a topic and all its subtopic through the protected administration API. 

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

You can compose subscriptions with the char '+' in order to merge notifications from multiple topics.

> curl -X GET http://localhost:8080/topics/customer/order/+/logistic/export

> ~~> Streaming subscription for topics /customer/order + /logistic/export with mode simple

Of course you are free to use reactive modes on composed subscriptions. Just be ready to handle the flow of data if you target a root topic with the replay mode :D

## Administration and statistics

Omnibus exposes usage statistics concerning all topics and the system itself following two modes.

- `simple` : get all statistics history available.
  - e.g  http://localhost:8080/stats/topics/customer/order/
  - e.g  http://localhost:8080/stats/system
- `streaming` : continous data stream of statistics in realtime
  - e.g  http://localhost:8080/stats/topics/customer/order?mode="streaming"
  - e.g  http://localhost:8080/stats/topics/system?mode="streaming"

## Usage and installation

There are two ways of running Omnibus

### As a standalone process.

Get the latest omnibus-standalone.jar or build from source with sbt assembly and run it.

```sh
java -jar omnibus-standalone.jar 8888 &
```

This starts Omnibus on port 8888 (if not specified as args, the default one is 8080).

### As an embedded library.

It is possible to integrate Omnibus in an existing Akka application.

Add the latest omnibus.jar to your application by building from source with sbt `publishLocal`

```
libraryDependencies += "com.agourlay" % "omnibus" % "0.1-SNAPSHOT"
```

and then call :

```scala
val receptionist : OmnibusReceptionist = omnibus.service.OmnibusBuilder.start(8080)
``` 

This will start the Omnibus system on the given port and return an OmnibusReceptionist object that offers all the useful methods to interact with the system :

```scala
  def createTopic(topicName : String, message : String)
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

### Optimistic roadmap

- [ ] provide minimal administration interface with statistics per topics 

- [ ] support properly Server-Sent-Event specification (Last-Event-Id header...)

- [ ] deploy bus in cluster (akka-cluster)

- [ ] support websockets