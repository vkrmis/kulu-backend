require 'rails_helper'

RSpec.describe Currency, :type => :model do
  it "fetches currencies from the backend service" do
    expected_currencies = ["USD", "INR"]
    expect_any_instance_of(KuluService::API).to receive(:list_currencies).and_return(expected_currencies)
    expect(Currency.all).to match_array(expected_currencies)
  end
end
