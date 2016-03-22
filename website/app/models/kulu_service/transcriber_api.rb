module KuluService
  class TranscriberAPI
    KULU_BACKEND_SERVICE_URL = ENV['KULU_BACKEND_SERVICE_URL']

    attr_reader :request

    def initialize
      @request = HTTPService::Request.new(KULU_BACKEND_SERVICE_URL)
    end

    def list_invoices(options)
      puts options
      page = (options[:page] || 1).to_i
      per_page = (options[:per_page] || Kaminari.config.default_per_page).to_i
      params = {page: page,
                per_page: per_page,
                order: (options[:sort] || 'created_at').downcase,
                user_email: options[:user_email],
                direction: (options[:direction] || 'desc').downcase}.merge(options)
      response = request.make(:get, 'transcriber', params, options[:token])
      MultiJson.load(response.body)
    end

    def find_invoice(options)
      options.merge!(user_email: options[:user_email])
      response = request.make(:get, "transcriber/#{options[:id]}", options, options[:token])
      MultiJson.load(response.body)
    end

    def next_and_prev_invoices(options)
      params = {user_email: options[:user_email], order: options[:order], direction: options[:direction]}
      response = request.make(:get, "transcriber/#{options[:id]}/next_and_prev_invoices", params, options[:token])
      MultiJson.load(response.body)
    end

    def update_invoice(options)
      response = request.make(:put, "transcriber/#{options[:id]}", {
                                      user_email: options[:user_email],
                                      invoice: options[:invoice].except(:id)
                                  }, options[:token])
      MultiJson.load(response.body)
    end

    def login(options)
      response = request.make(:post, 'transcriber/login', params: options)
      MultiJson.load(response.body)
    end
  end
end
