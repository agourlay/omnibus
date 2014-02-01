App.IndexView = Em.View.extend({
    tagName : 'div',
    elementId: 'summary',
    contentBinding: 'controller.content',

    didInsertElement: function() {
        var view = this;
        console.dir(view.content)
    }    
})    