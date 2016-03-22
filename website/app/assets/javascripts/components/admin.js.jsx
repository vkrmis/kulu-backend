var React = require('react');
var Tabs = require('react-simpletabs');
var Records = require('./records.js.jsx');
var Invite = require('./invite.js.jsx');

var Admin = React.createClass({
  getInitialState: function (e) {
    return {
      users: [],
      activeTab: 1
    }
  },

  render: function () {
    var forwardMail = "expenses." + this.props.auth.organization_name + "@kulu.in";

    return (<div className="invite">
      <Tabs onBeforeChange={this.handleBefore} tabActive={this.state.activeTab}>
        <Tabs.Panel title='Categories'>
          <h4>Add new</h4>
          <Records auth={this.props.auth}/>
        </Tabs.Panel>

        <Tabs.Panel title='Invite'>
          <Invite auth={this.props.auth}/>
        </Tabs.Panel>

        <Tabs.Panel title='Help'>
          <div className="help-container">
            <h3>Send receipts by email</h3>

            <div className='mail-receipt-container'>
              <div className="receipt-icon"><i className="fa fa-envelope-o"></i></div>
              <p>
                You may also forward your inbox receipts to <a href={"mailto:" + forwardMail}
                                                               target="_top">{forwardMail}</a><br/> using
                your account email.
              </p>
              <br/>

              <p>
                We'll process them and put them in Kulu.
              </p>
            </div>

            <h3>Reach out to us</h3>

            <p>If you're facing any issues in using Kulu, feel free to drop us a <a href="mailto:kulu@nilenso.com"
                                                                                    target="_top">mail</a>.
              Or call us at <a href="tel:+918884100170">+91 8884 100 170</a> / <a href="tel:+918553427344">+91 8553 427 344</a></p>
          </div>
        </Tabs.Panel>
      </Tabs>
    </div>);
  }
});

module.exports = Admin;
