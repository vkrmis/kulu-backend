require 'rails_helper'
require 'http_service/error'

RSpec.describe HTTPService::Response, :type => :model do
  context 'check errors' do
    it 'does not return any errors for a 2xx response' do
      response_2xx = HTTPService::Response.new(200, 'bogus', {})
      expect(response_2xx).to be_a_kind_of(HTTPService::Response)
    end

    it 'raises a ClientError for a 4xx response' do
      expect {
        HTTPService::Response.new(400, 'bogus', {})
      }.to raise_error(HTTPService::ClientError)
    end

    it 'raises a ServerError for a 5xx response' do
      expect {
        HTTPService::Response.new(501, 'bogus', {})
      }.to raise_error(HTTPService::ServerError)
    end

    it 'raises a BadResponse for a successful response with an empty response body' do
      expect {
        HTTPService::Response.new(201, '', {})
      }.to raise_error(HTTPService::BadResponse)
    end
  end
end
