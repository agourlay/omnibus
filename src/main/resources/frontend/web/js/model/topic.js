App.Topic = Em.Object.extend({
        name : null,
        subTopics: [],
        subscribersNumber : null,
        eventsNumber : null,
        loaded: false,
        
        subTopicsNumber: function(){
            return this.subTopics.length ;
        }.property("loaded")
});