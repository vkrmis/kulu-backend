module HTTPService
  require 'http_service/error'

  class Response
    attr_reader :status, :body, :headers

    def initialize(status, body, headers)
      @status = status.to_i
      @body = body
      @headers = headers

      check_errors
    end

    private

    def check_errors
      if status >= 400
        if status >= 500
          raise(ServerError.new(status, body))
        else
          raise(ClientError.new(status, body))
        end
      end

      if body.empty? && status != 204
        raise(BadResponse.new(status, body))
      end
    end
  end
end
