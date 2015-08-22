App.TopicsController = Ember.ArrayController.extend({
  sortProperties: ['name'],
  sortAscending: true,

  actions : {
    createTopic : function(newTopic) {
      var controller = this;
      if (newTopic) {
        App.Dao.postTopic(newTopic).done(function() {
          controller.reloadContent();
        });
      }  
    },

    deleteTopic : function(topicName) {
      var controller = this;
      if (topicName) {
        App.Dao.deleteTopic(topicName).done(function() {
          controller.reloadContent();
        });
      }  
    }
  },

  expandTopic: function(topicsName){
    var controller = this;
    $.each(topicsName, function(i, topic){
      var exist = controller.findBy('name', topic);
      if (!exist) {
        App.Dao.topic(topic).then(function (sub) {
          controller.get('model').pushObject(sub); 
        });
      }
    });
  },

  collapseTopic: function(topicName, subTopicsName){
    var controller = this;
    var toDelete = [];
    $.each(controller.get("model"), function(i, topic){
      $.each(subTopicsName, function(i, sub){
        if (topic.name.indexOf(sub) == 0 ) {
          toDelete.push(topic);
        }
      }); 
    }); 
    controller.get('model').removeObjects(toDelete); 
  },

  reloadContent : function() {
    var controller = this;
    App.Dao.topics().then(function(topics){
      controller.get('model').clear();
      controller.get('model').pushObjects(topics);  
    });
  },
})