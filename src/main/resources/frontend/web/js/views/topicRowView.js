App.TopicRowView = Em.View.extend({
	tagName: 'tr',
    topicv: null,
    isExpanded : false,

    actions : {
    	expand: function() {
		    var topic = this.get('topicv');
		    if (this.get("isExpanded")){
		    	this.get("controller").collapseTopic(topic.name, topic.get("subTopics"));
		    	this.set("isExpanded", false);
		    } else {
		    	this.get("controller").expandTopic(topic.get("subTopics"));
		    	this.set("isExpanded", true);
		    }
	    }
    }
})