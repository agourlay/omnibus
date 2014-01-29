App.Dao = Em.Object.create({

	topics : function()  {
        var topicsModel = Ember.A([]);
        $.ajax({
            url: "topics/",
            type: 'GET',
            success: function(topics) {
                $.each( topics, function(i, topic){
                	console.dir(topic)
                    var model = App.Topic.create({
	                    name: topic.topic.toString(),
	                    eventsNumber : topic.eventsNumber,
	                    subscribersNumber : topic.subscribersNumber,
	                    subTopics : topic._embedded.children.map(function(obj){ 
                            return topic.topic.toString() + "/" +Object.keys(obj); 
                        })
                    });
                    console.dir(model)
                    topicsModel.pushObject(model);        
                });                                        
            },
            error: function(xhr, ajaxOptions, thrownError) {
                console.log("Error during topics retrieval");                                        
            }
        });
        return topicsModel;
	}

});

