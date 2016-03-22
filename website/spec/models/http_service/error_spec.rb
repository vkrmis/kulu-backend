require 'rails_helper'

RSpec.describe HTTPService::Error, :type => :model do
  it 'builds a message from the response body and http code for the error' do
    error = HTTPService::Error.new(500, {:errors => {:jacob => 'god loves you as he loved jacob'}}.to_json)

    expect(error.to_s).to eq('jacob: god loves you as he loved jacob [HTTP 500]')
    expect(error.http_status).to eq(500)
    expect(error.response_body).to eq({'errors' => {'jacob' => 'god loves you as he loved jacob'}})
  end
end
