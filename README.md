## omnibus [![Build Status](https://travis-ci.org/agourlay/omnibus.png?branch=master)](https://travis-ci.org/agourlay/omnibus)

 * warning : this is a BIG MESS in progress *

 A reactive Http message bus.

#### features

Push text/json messages on a rest api and receive subscribtion by server-sent-event

Topics are trees, you can create them easily

> curl -X POST http://localhost:8080/topics/example/nested
 Create the nested topic "/example/nested"

You can push data to the topic

> curl -X PUT http://localhost:8080/topics/example/nested -d "a message"
Publish the text message on the topic

And finally you can subscribe
> curl -X GET http://localhost:8080/topics/example/nested
>... Streaming subscription for topics /example/nested

#### more features

You can compose subscriptions

> curl -X GET http://localhost:8080/topics/example/nested+another/examples
> ... Streaming subscription for topics /example/nested + /another/examples
Here you are subscribing to 2 differents topics using '+'

Omnibus supports reactive modes in order to retrieve more infos from topics.

The supported mode are: 

- simple : regular subscription (default one)
- last   : get last message on topic and following
- replay  : get all past message on topic and then

Modes are specified by url parameter
> curl -X GET http://localhost:8080/topics/example/nested
> ... Streaming subscription for topics /example/nested?mode=replay
> data: {"topicName":"example/nested","payload":"old message","timestamp":1387813403}

#### roadmap

- support completely Server-Sent-Event specification (Last-event-Seen header)

- provide statistics per topics

- deploy bus in cluster

- support other protocols (websockets)



