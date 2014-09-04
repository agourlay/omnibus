App.SubscriptionsController = Ember.ArrayController.extend({	
  sortProperties: ['topic'],
  sortAscending: true,

  actions: {  
    deleteSubscriber : function(subId) {
      var controller = this;
      if (subId) {
        App.Dao.deleteSubscriber(subId).done(function() {
          controller.reloadContent();
        });
      }  
    }
  },

  reloadContent : function() {
    var controller = this;
    App.Dao.subscribers().then(function(topics){
      controller.model.clear();
      controller.model.pushObjects(topics);  
    });
  }
})