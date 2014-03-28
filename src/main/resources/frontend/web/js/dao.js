App.Dao = Em.Object.create({

    sourceSSE : null,
    eventBus : null,

    getJSON : function(url) {
      var promise = new Ember.RSVP.Promise(function(resolve, reject){
        var client = new XMLHttpRequest();
        client.open("GET", url);
        client.onreadystatechange = handler;
        client.responseType = "json";
        client.setRequestHeader("Accept", "application/json");
        client.send();

        function handler() {
          if (this.readyState === this.DONE) {
            if (this.status === 200) { resolve(this.response); }
            else { reject(this); }
          }
        };
      });

      return promise;
    },

    setupStream : function (streamUrl) {
        var me = this;
        // close previous stream if any
        if (me.get("sourceSSE") != null) {
            me.get("sourceSSE").close();
        }

        me.set('eventBus', new Bacon.Bus());

        var source = new EventSource(streamUrl);
        source.addEventListener('message', function(e) {
            var data = $.parseJSON(e.data);
            me.get("eventBus").push(data);
        }, false);

        source.addEventListener('open', function(e) {
            console.log("Streaming open")
        }, false);

        source.addEventListener('error', function(e) {
            if (e.readyState == EventSource.CLOSED) {
                errorMessage("Streaming service error");
            }
        }, false);

        me.set("sourceSSE", source);
    },

    summary :function() {
        var dao = this;
        var systemPromise = dao.system();
        var topicsPromise = dao.topics();
        var subscribersPromise = dao.subscribers();
        return $.when(systemPromise, topicsPromise, subscribersPromise).then(function (system, topics, subs) {
            var model = App.Summary.create();
            model.set('system', system);
            model.set('rootTopics', topics);
            model.set('subscriptions', subs);
            return model;
        });
    },

    system : function()  {
        var dao = this;
        return $.ajax({
            url: "stats/system",
            type: 'GET',
            error: function(xhr, ajaxOptions, thrownError) {
                console.log("Error during current system stat retrieval");                                        
            }
        }).then(function (data) {return dao.createSystemModel(data)});
    },

    systemStats : function() {
        var dao = this;
        return this.getJSON("stats/system?mode=history")
                    .then(function (stats) {
                        var systemStatsModel = Ember.A([]);
                        $.each( stats, function(i, stat){
                            var model = dao.createSystemModel(stat)
                            systemStatsModel.pushObject(model);        
                        });
                        return systemStatsModel;
                    }, function(error) { 
                        return Ember.A([]);
                    });
    }, 

    subscribers : function() {
        var dao = this;
        return $.ajax({
            url: "admin/subscribers",
            type: 'GET',
            error: function(xhr, ajaxOptions, thrownError) {
                console.log("Error during subscribers retrieval");                                        
            }
        }).then(function (subs) {
            var subsModel = Ember.A([]);
            $.each(subs, function(i, sub){
                var model = dao.createSubscriberModel(sub)
                subsModel.pushObject(model);        
            });
            return subsModel;
        });
    },      

    topicStats : function(topicName)  {
        var dao = this;
        return this.getJSON("stats/topics/"+topicName+"?mode=history")
                    .then(function (topicStats) {
                        var container = App.TopicStatContainer.create();
                        var topicStatsModel = Ember.A([]);
                        $.each( topicStats, function(i, topicStat){
                            var model = dao.createTopicStatModel(topicStat);
                            topicStatsModel.pushObject(model);        
                        });
                        container.set("topic", topicName);
                        container.set("stats", topicStatsModel); 
                        return container
                    }, function(error) { 
                        var container = App.TopicStatContainer.create();
                        container.set("topic", topicName);
                        container.set("stats", Ember.A([]));
                        console.log(container) ;
                        return container ;
                    });
    },
   
    createTopicStatModel : function(topicStat) {
        var model = App.TopicStat.create();
        model.set('topic', topicStat.topic);
        model.set('throughputPerSec', topicStat.throughputPerSec);
        model.set('subscribersNumber', topicStat.subscribersNumber);
        model.set('subTopicsNumber', topicStat.subTopicsNumber);
        model.set('timestamp', topicStat.timestamp);
        return model;
    },

    createSubscriberModel : function(sub) {
        var model = App.Subscriber.create();
        model.set('topic', sub.topic);
        model.set('id', sub.id);
        model.set('ip', sub.ip);
        model.set('mode', sub.mode);
        model.set('creationDate', sub.creationDate);
        return model;
    },

    createSystemModel : function(system) {
        var model = App.System.create();
        model.set('totalRequests', system.totalRequests);
        model.set('openRequests', system.openRequests);
        model.set('maxOpenRequests', system.maxOpenRequests);
        model.set('totalConnections', system.totalConnections);
        model.set('openConnections', system.openConnections);
        model.set('maxOpenConnections', system.maxOpenConnections);
        model.set('requestTimeouts',system.requestTimeouts);
        model.set('uptime',moment.duration(system.uptimeInMilli).humanize());
        model.set('timestamp', system.timestamp);
        return model;
    },

	topics : function()  {
        var dao = this;
        return $.ajax({
            url: "topics",
            type: 'GET',
            error: function(xhr, ajaxOptions, thrownError) {
                console.log("Error during topics retrieval");                                        
            }
        }).then(function (topics) {
            var topicsModel = Ember.A([]);
            $.each( topics, function(i, topic){
                var model = dao.createTopicModel(topic);
                topicsModel.pushObject(model);        
            });   
            return topicsModel
        });
	},

    topic : function (topicName) {
        var dao = this;
        return $.ajax({
            url: "topics/"+topicName,
            type: 'GET',
            error: function(xhr, ajaxOptions, thrownError) {
                console.log("Error during topic retrieval");                                        
            }
        }).then(function (data) {return dao.createTopicModel(data)});
    },

    createTopicModel : function(topic) {
        var model = App.Topic.create();
        model.set("name", topic.topic.join().replace(/,/g,"/"));
        model.set("eventsNumber", topic.eventsNumber);
        model.set("subscribersNumber", topic.subscribersNumber);
        model.set("creationDate", topic.creationDate);
        model.set("subTopics", topic._embedded.children.map(function(obj){ 
            return model.get("name") + "/" +Object.keys(obj); 
        }))
        return model;
    },

    postTopic : function(topicName) {
        return $.ajax({
            url: "/topics/"+topicName,
            method: "POST",
            contentType: "application/json"
        });
    },

    deleteTopic : function(topicName) {
        return $.ajax({
            url: "/admin/topics/"+topicName,
            method: "DELETE",
            contentType: "application/json"
        });
    },

    deleteSubscriber : function(subId) {
        return $.ajax({
            url: "/admin/subscribers/"+subId,
            method: "DELETE",
            contentType: "application/json"
        });
    }
});