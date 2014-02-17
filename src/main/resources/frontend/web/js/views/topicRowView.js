App.TopicRowView = Em.View.extend({
	tagName: 'tr',
    topic: null,
    isExpanded : false,

	expand: function() {
	    var topic = this.get('topic');
	    if (this.get("isExpanded")){
	    	this.get("controller").collapseTopic(topic.name, topic.subTopics);
	    	this.set("isExpanded", false);
	    } else {
	    	this.get("controller").expandTopic(topic.subTopics);
	    	this.set("isExpanded", true);
	    }
    }
})