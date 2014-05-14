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
        var topicsPromise = dao.topics();
        var subscribersPromise = dao.subscribers();
        return $.when(topicsPromise, subscribersPromise).then(function (topics, subs) {
            var model = App.Summary.create();
            model.set('rootTopics', topics);
            model.set('subscriptions', subs);
            return model;
        });
    },

    system : function()  {
        var dao = this;
        return $.ajax({
            url: "stats/metrics",
            type: 'GET',
            error: function(xhr, ajaxOptions, thrownError) {
                console.log("Error during current system stat retrieval");                                        
            }
        }).then(function (json) {
            var system = App.System.create();
            var meters = dao.buildMetric(json, 5);
            var timers = dao.buildMetric(json, 9);
            var counters = dao.buildMetric(json, 1);
            system.set('meters', meters);
            system.set('timers', timers);
            system.set('counters', counters);
            return system;
        });
    },

    buildMetric : function (json, keyNb) {
        var filtered = this.filterByFieldNumber(json, keyNb);
        var metrics = [];
        jQuery.each(filtered, function(i, val) {
            var newMetric = new Object();;
            if( keyNb == 5) { newMetric = App.Meter.create(val.value); }
            if( keyNb == 1) { newMetric = App.Counter.create(val.value); }
            if( keyNb == 9) { newMetric = App.Timer.create(val.value); }    
            newMetric.name = val.name;
            metrics.push(newMetric);
        });
        return metrics.sort(function(a, b){return (a.name < b.name)?-1:1});
    },

    filterByFieldNumber : function (jsonObj, n) {
        var filtered = [];
        jQuery.each(jsonObj, function(i, val) {
            if(Object.keys(val).length == n) {
                var container = new Object();
                container.name = i;
                container.value = val;
                filtered.push(container);
            }
        });
        return filtered;
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

    createSubscriberModel : function(sub) {
        var model = App.Subscriber.create();
        model.set('topic', sub.topic);
        model.set('id', sub.id);
        model.set('ip', sub.ip);
        model.set('mode', sub.mode);
        model.set('creationDate', sub.creationDate);
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
        model.set('throughputPerSec', topic.throughputPerSec);
        model.set("creationDate", topic.creationDate);
        model.set("timestamp", topic.timestamp);
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