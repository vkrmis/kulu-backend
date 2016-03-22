var React = require('react');

var amountFormat;

amountFormat = function (amount) {
  return '$ ' + Number(amount).toLocaleString();
};

var Record = React.createClass({
  getInitialState: function () {
    return {
      edit: false
    };
  },

  handleToggle: function (e) {
    e.preventDefault();
    return this.setState({
      edit: !this.state.edit
    });
  },

  handleDelete: function (e) {
    var self = this;
    e.preventDefault();

    if (!window.confirm("Are you sure?")) {
      e.stopPropagation();
      return false;
    }

    return $.ajax({
      method: 'DELETE',
      url: "/categories/" + this.props.record.id,
      dataType: 'JSON'
    }).success(function () {
        return self.props.handleDeleteRecord(self.props.record);
      }
    );
  },

  handleEdit: function (e) {
    var self = this;
    e.preventDefault();

    var sendData = {
      name: React.findDOMNode(this.refs.name).value
    };

    return $.ajax({
      method: 'PUT',
      url: "/categories/" + this.props.record.id,
      dataType: 'JSON',
      data: {
        name: sendData.name
      }
    }).success(function () {
        self.setState({edit: false});
        return self.props.handleEditRecord(self.props.record, sendData);
      }
    );
  },

  recordRow: function () {
    return React.DOM.tr(null, React.DOM.td(null, this.props.record.name), React.DOM.td(null, React.DOM.a({
      className: 'btn btn-default',
      onClick: this.handleToggle
    }, React.DOM.i({className: 'fa fa-pencil-square-o'})), React.DOM.a({
      className: 'btn btn-danger',
      onClick: this.handleDelete
    }, React.DOM.i({className: 'fa fa-trash-o'}))));
  },

  recordForm: function () {
    return React.DOM.tr(null, React.DOM.td(null, React.DOM.input({
      className: 'form-control',
      type: 'text',
      defaultValue: this.props.record.name,
      ref: 'name'
    })), React.DOM.td(null, React.DOM.a({
      className: 'btn btn-default',
      onClick: this.handleEdit
    }, React.DOM.i({className: 'fa fa-check-square'})), React.DOM.a({
      className: 'btn btn-danger',
      onClick: this.handleToggle
    }, React.DOM.i({className: 'fa fa-times'}))));
  },

  render: function () {
    if (this.state.edit) {
      return this.recordForm();
    } else {
      return this.recordRow();
    }
  }
});

module.exports = Record;
