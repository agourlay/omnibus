module.exports = function(grunt) {
  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    emberTemplates: {
       compile: {
        options: {
          templateBasePath: /web\/templates\//,
          templateFileExtensions: /\.hbs/
        },
        files: {
          "web/dist/js/templates.js": ["web/templates/**/*.hbs"]
        }
      }
    },
    concat : {
      libjs : {
        src : [
          "web/bower_components/jquery/dist/jquery.min.js",
          "web/bower_components/handlebars/handlebars.runtime.min.js",
          "web/bower_components/ember/ember.min.js",
          "web/bower_components/momentjs/min/moment.min.js",
          "web/bower_components/bacon/dist/Bacon.min.js",
          "web/bower_components/d3/d3.min.js",
          "web/bower_components/rickshaw/rickshaw.min.js"
        ],
        dest: 'web/dist/js/libs.min.js'
      },
      libcss : {
        src : [
          "web/bower_components/pure/pure-min.css",
          "web/bower_components/rickshaw/rickshaw.min.css",
          "web/bower_components/font-awesome/css/font-awesome.min.css"
        ],
        dest : 'web/dist/css/libs.min.css'
      }
    },
    uglify: {
      js: {
        files: {
          'web/dist/js/omnibus.min.js': [
            "web/dist/js/templates.js",
            "web/js/app.js",
            "web/js/dao.js",
            "web/js/router.js",
            "web/js/views/*",
            "web/js/models/*",
            "web/js/controllers/*"
          ]
        }
      }
    },
    cssmin : {
      combine: {
        files: {
          "web/dist/css/omnibus.min.css" : [
            "web/css/layout.css"
          ]
        } 
      }   
    },
    copy: {
      fonts: {
        src: 'web/bower_components/font-awesome/fonts/*',
        dest: "web/dist/fonts/",
        filter: 'isFile',
        flatten: true,
        expand: true
      },
      maps: {
        cwd : "web/bower_components/jquery/dist/",
        src: "jquery.min.map",
        dest: "web/dist/js/",
        expand: true
      }
    },
    watch: {
      files: ["web/css/**","web/js/**","web/templates/**"],
      tasks: ['default']
    },
    htmlmin: {                                     
      dist: {                                      
        options: {                                 
          removeComments: true,
          collapseWhitespace: true
        },
        files: {                                   
          'web/dist/index.html': 'web/index.html'
        }
      }
    }
  });

  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-contrib-concat');
  grunt.loadNpmTasks('grunt-contrib-cssmin');
  grunt.loadNpmTasks('grunt-ember-templates');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-contrib-htmlmin');

  // Default task(s).
  grunt.registerTask('default', ['emberTemplates','concat','uglify','cssmin','copy', 'htmlmin']);

};