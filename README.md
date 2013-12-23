# Omnibus [![Build Status](https://travis-ci.org/agourlay/omnibus.png?branch=master)](https://travis-ci.org/agourlay/omnibus)

**WARNING : THIS IS A BIG MESS IN PROGRESS**

An awesome reactive HTTP message bus.

Push text messages using an HTTP api and receive subscriptions streamed by server-sent-event

*tl;dr : you can easily create huge hierarchy of nested topics and then push messages or listen at any level in the tree.* 

### Classic bus features

Topics are trees, you can create them easily with a POST request.

Create the nested topic "/example/nested"

> curl -X POST http://localhost:8080/topics/example/nested

You can push data to the topic.

> curl -X PUT http://localhost:8080/topics/example/nested -d "a message"

If you publish a message at the "/example" level, all subtopics will receive it as well.

And finally you can subscribe to notification on a topic

> curl -X GET http://localhost:8080/topics/example/nested

>... Streaming subscription for topics /example/nested

### More great features

You can compose subscriptions with the char '+'

> curl -X GET http://localhost:8080/topics/example/nested+another/examples

> ... Streaming subscription for topics /example/nested + /another/examples

Omnibus supports reactive modes in order to retrieve more infos from topics.

The supported mode are: 

- simple : regular subscription (default one if not specified)
- last   : get last message on a topic and the following
- replay : get all past message on topic and the following

Modes are specified by url parameter
> curl -X GET http://localhost:8080/topics/example/nested

> ... Streaming subscription for topics /example/nested?mode=replay

> data: {"topicName":"example/nested","payload":"old message","timestamp":1387813403}

Of course you are free to use reactive modes on composed subscriptions.

> curl -X GET http://localhost:8080/topics/example/nested+stuff/blabla+thingy/blabla?mode=replay

### Unrealistic roadmap

- support completely Server-Sent-Event specification (Last-event-Seen header)

- provide statistics per topics

- persist content of topic for crash recovery

- deploy bus in cluster

- support other protocols (websockets)
