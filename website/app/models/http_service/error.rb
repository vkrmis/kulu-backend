module HTTPService
  class Error < StandardError
    attr_accessor :response_body, :http_status

    def initialize(http_status, response_body)
      self.http_status = http_status

      begin
        self.response_body = MultiJson.load(response_body)
      rescue MultiJson::DecodeError
        self.response_body = {}
      end

      error_info = self.response_body['errors'] || {}

      error_array = error_info.inject([]) { |a, (k, v)| a << "#{k}: #{v}" }

      if error_array.empty?
        message = self.response_body.to_s
      else
        message = error_array.join(', ')
      end

      message += " [HTTP #{http_status}]"

      super(message)
    end

  end

  # Any invalid/empty response body
  class BadResponse < Error
  end

  # Any error with a 5xx HTTP status code
  class ServerError < Error
  end

  # Any error with a 4xx HTTP status code
  class ClientError < Error
  end
end
