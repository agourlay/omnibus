App.Subscriber = Em.Object.extend({
    topic : null,
    ip : null,
    creationDate: null,

    prettyCreationDate : function(){
    	return moment.unix(this.creationDate).fromNow();
    }.property("creationDate")
});