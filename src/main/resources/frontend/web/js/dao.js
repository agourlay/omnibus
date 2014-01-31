App.Dao = Em.Object.create({

	topics : function()  {
        var dao = this;
        return $.ajax({
            url: "topics/",
            type: 'GET',
            error: function(xhr, ajaxOptions, thrownError) {
                console.log("Error during topics retrieval");                                        
            }
        }).then(function (topics) {
            var topicsModel = Ember.A([]);
            $.each( topics, function(i, topic){
                var model = App.Topic.create();
                dao.createTopicModel(topic, model);
                topicsModel.pushObject(model);        
            });   
            return topicsModel
        });
	},

    topic : function (topicName) {
        var model = App.Topic.create();
        var dao = this;
        return $.ajax({
            url: "topics/"+topicName,
            type: 'GET',
            error: function(xhr, ajaxOptions, thrownError) {
                console.log("Error during topic retrieval");                                        
            }
        }).then(function (data) {return dao.createTopicModel(data, model)});
    },

    createTopicModel : function(topic, model) {
        model.set("name", topic.topic.join().replace(",","/"));
        model.set("eventsNumber", topic.eventsNumber);
        model.set("subscribersNumber", topic.subscribersNumber);
        model.set("creationDate", topic.creationDate);
        model.set("subTopics", topic._embedded.children.map(function(obj){ 
            return model.get("name") + "/" +Object.keys(obj); 
        }))
        return model;
    }

});

