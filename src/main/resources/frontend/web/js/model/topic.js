App.Topic = Em.Object.extend({
        name : null,
        subTopics: [],
        subscribersNumber : null,
        eventsNumber : null,
        creationDate: null,
        
        subTopicsNumber: function(){
            return this.subTopics.length ;
        }.property("loaded"),

        prettyCreationDate : function(){
        	return moment.unix(this.creationDate).fromNow();
        }.property("creationDate")
});