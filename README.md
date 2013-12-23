# Omnibus [![Build Status](https://travis-ci.org/agourlay/omnibus.png?branch=master)](https://travis-ci.org/agourlay/omnibus)

**WARNING : this is a BIG MESS in progress**

An awesome reactive HTTP message bus.

### features

Push unstructured text messages on a rest api and receive subscribtion streamed by server-sent-event

Topics are trees, you can create them easily

Create the nested topic "/example/nested"

> curl -X POST http://localhost:8080/topics/example/nested

You can push data to the topic

> curl -X PUT http://localhost:8080/topics/example/nested -d "a message"

And finally you can subscribe
> curl -X GET http://localhost:8080/topics/example/nested
>... Streaming subscription for topics /example/nested

### More features

You can compose subscriptions with the char '+'

> curl -X GET http://localhost:8080/topics/example/nested+another/examples

> ... Streaming subscription for topics /example/nested + /another/examples

Omnibus supports reactive modes in order to retrieve more infos from topics.

The supported mode are: 

- simple : regular subscription (default one)
- last   : get last message on topic and following
- replay  : get all past message on topic and then

Modes are specified by url parameter
> curl -X GET http://localhost:8080/topics/example/nested

> ... Streaming subscription for topics /example/nested?mode=replay

> data: {"topicName":"example/nested","payload":"old message","timestamp":1387813403}

### Unrealistic roadmap

- support completely Server-Sent-Event specification (Last-event-Seen header)

- provide statistics per topics

- deploy bus in cluster

- support other protocols (websockets)



