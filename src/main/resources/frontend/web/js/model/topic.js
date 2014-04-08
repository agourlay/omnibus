App.Topic = Em.Object.extend({
    name : null,
    subTopics: [],
    subscribersNumber : null,
    eventsNumber : null,
    throughputPerSec : null,
    creationDate: null,
    timestamp : 0,
    
    subTopicsNumber: function(){
        return this.subTopics.length ;
    }.property("subTopics"),

    hasSubTopics: function(){
        return this.subTopics.length > 0;
    }.property("subTopics"),

    prettyCreationDate : function(){
    	return moment.unix(this.creationDate).fromNow();
    }.property("creationDate")
});