require 'rails_helper'

RSpec.describe KuluService::API, :type => :model do
  it "fetches currencies from the backend" do
    currencies = ["USD", "INR"]
    response = HTTPService::Response.new(200, currencies.to_json, {})
    expect_any_instance_of(HTTPService::Request).to receive(:make).with(:get, "currencies") \
      .and_return(response)
    expect(KuluService::API.new.list_currencies).to match_array(currencies)
  end
end
