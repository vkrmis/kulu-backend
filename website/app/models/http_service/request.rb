module HTTPService
  class Request
    attr_reader :connection

    def initialize(url)
      @connection = Faraday.new(:url => url) do |faraday|
        faraday.request(:url_encoded)
        faraday.response(:logger)
        faraday.adapter(Faraday.default_adapter)
      end
    end

    def make(verb, request_url, request_body = {}, token='')
      response = send(verb, request_url, request_body, token)
      Response.new(response.status, response.body, response.headers)
    end

    def post(request_url, request_body = {}, token)
      connection.post do |req|
        req.url(request_url)
        req.body = request_body.to_json
        req.headers['Content-Type'] = 'application/json'
        req.headers['X-Auth-Token'] = token
      end
    end

    def put(request_url, request_body = {}, token)
      connection.put do |req|
        req.url(request_url)
        req.body = request_body.to_json
        req.headers['Content-Type'] = 'application/json'
        req.headers['X-Auth-Token'] = token
      end
    end

    def get(request_url, request_body = {}, token)
      connection.get do |req|
        req.url(request_url)
        req.params = request_body
        req.headers['X-Auth-Token'] = token
      end
    end

    def delete(request_url, request_body = {}, token)
      connection.delete do |req|
        req.url(request_url)
        req.params = request_body
        req.headers['Content-Type'] = 'application/json'
        req.headers['X-Auth-Token'] = token
      end
    end
  end
end
