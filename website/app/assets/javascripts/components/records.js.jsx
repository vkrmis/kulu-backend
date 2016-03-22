var React = require('react/addons');
var RecordForm = require('./record_form.js.jsx');
var Record = require('./record.js.jsx');
var LoadingIcon = require('./loading-icon.js.jsx');

var Records = React.createClass({
  getInitialState: function () {
    return {
      records: []
    };
  },

  getDefaultProps: function () {
    return {
      auth: {}
    };
  },

  listCategories: function () {
    var self = this;

    $.get('/categories', {
      token: this.props.auth.token,
      organization_name: this.props.auth.organization_name
    }).fail(function (e) {
      console.log(e);
    }).success(function (d) {
      self.setState({records: d});
    });
  },

  componentWillMount: function() {
    this.listCategories();
  },

  addRecord: function (record) {
    var records;

    records = React.addons.update(this.state.records, {
      $push: record
    });

    return this.setState({
      records: records
    });
  },

  deleteRecord: function (record) {
    var index, records;
    index = this.state.records.indexOf(record);

    records = React.addons.update(this.state.records, {
      $splice: [[index, 1]]
    });

    return this.setState({
      records: records
    });
  },

  updateRecord: function (record, data) {
    var index, records;
    index = this.state.records.indexOf(record);

    var recordItr = {};
    recordItr[index] = {$merge: data};
    records = React.addons.update(this.state.records, recordItr);

    return this.setState({
      records: records
    });
  },

  render: function () {
    var record;

    if (!this.state.records) {
      return <LoadingIcon/>
    }

    return React.DOM.div({
      className: 'records'
    }, React.createElement(RecordForm, {
      handleNewRecord: this.addRecord
    }), React.DOM.table({
        className: 'table table-bordered'
      }, React.DOM.thead(null,
        React.DOM.tr(null,
          React.DOM.th(null, 'Name'),
          React.DOM.th({style: {width: "12%"}}, 'Actions'))),
      React.DOM.tbody(null, (function () {
        var i, len, ref, results;
        ref = this.state.records;
        results = [];
        for (i = 0, len = ref.length; i < len; i++) {
          record = ref[i];
          results.push(React.createElement(Record, {
            key: record.id,
            record: record,
            auth: this.props.auth,
            handleDeleteRecord: this.deleteRecord,
            handleEditRecord: this.updateRecord
          }));
        }
        return results;
      }).call(this))));
  }
});

module.exports = Records;
