App.Subscriber = Em.Object.extend({
    topic : null,
    ip : null,
    mode : null,
    support : null,
    creationDate: null,

    prettyCreationDate : function(){
    	return moment.unix(this.creationDate).fromNow();
    }.property("creationDate")
});