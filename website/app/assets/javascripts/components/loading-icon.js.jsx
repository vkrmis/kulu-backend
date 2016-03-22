var div, img, ref;

ref = React.DOM;
div = ref.div;
img = ref.img;

module.exports = React.createClass({
  displayName: 'LoadingIcon',
  render: function() {
    return div({
      className: 'loading-icon'
    }, img({
      className: 'loading-icon-image',
      src: '/assets/loading-icon.gif'
    }));
  }
});

