App.Subscriber = Em.Object.extend({
    topic : null,
    ip : null,
    mode : null,
    creationDate: null,

    prettyCreationDate : function(){
    	return moment.unix(this.creationDate).fromNow();
    }.property("creationDate")
});