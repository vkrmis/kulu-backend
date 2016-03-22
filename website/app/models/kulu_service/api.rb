module KuluService
  class API
    KULU_BACKEND_SERVICE_URL = ENV['KULU_BACKEND_SERVICE_URL']

    attr_reader :request

    def initialize
      @request = HTTPService::Request.new(KULU_BACKEND_SERVICE_URL)
    end

    def create_invoice(options)
      ##
      #
      # FIXME:
      # Because we updated the create API to take these extra parameters without making them optional,
      # I'm changing it here because the change required in the backend will break the mobile client.
      # We need to fix it on the backend and update all the clients.
      # Until then, this hack will do - kit
      #
      stubbed_parameters = {remarks: '', expense_type: '', date: Date.today.iso8601}
      #
      ##
      params = {
          organization_name: options[:organization_name],
          :invoice => {
              storage_key: options[:invoice][:storage_key],
              user_token: options[:token]}.merge(stubbed_parameters)}

      response = request.make(:post, 'invoices', params, options[:token])
      MultiJson.load(response.body)['id']
    end

    def list_invoices(options)
      page = (options[:page] || 1).to_i
      per_page = (options[:per_page] || Kaminari.config.default_per_page).to_i
      params = {organization_name: options[:organization_name],
                page: page,
                per_page: per_page,
                order: (options[:sort] || 'created_at').downcase,
                direction: (options[:direction] || 'desc').downcase}.merge(options)
      response = request.make(:get, 'invoices', params, options[:token])
      MultiJson.load(response.body)
    end

    def search(options)
      page = (options[:page] || 1).to_i
      per_page = (options[:per_page] || Kaminari.config.default_per_page).to_i
      params = {page: page,
                per_page: per_page}.merge(options)
      response = request.make(:get, 'invoices/search', params, options[:token])
      MultiJson.load(response.body)
    end

    def find_invoice(options)
      response = request.make(:get, "invoices/#{options[:id]}", options, options[:token])
      MultiJson.load(response.body)
    end

    def update_invoice(options)
      invoice_params = options[:invoice].except(:id).delete_if { |_, v| v.blank? }
      response = request.make(:put, "invoices/#{options[:invoice][:id]}", {
                                      organization_name: options[:organization_name], invoice: invoice_params
                                  }, options[:token])
      MultiJson.load(response.body)
    end

    def delete_invoice(options)
      response = request.make(:delete, "invoices/#{options[:id]}", {
                                         organization_name: options[:organization_name]
                                     }, options[:token])
      response.status == 204
    end

    def list_of_states(options)
      response = request.make(:get, 'invoices/states', {
                                      organization_name: options[:organization_name]
                                  }, options[:token])
      MultiJson.load(response.body)
    end

    def next_and_prev_invoices(options)
      params = {organization_name: options[:organization_name], order: options[:order], direction: options[:direction]}
      response = request.make(:get, "invoices/#{options[:id]}/next_and_prev_invoices", params, options[:token])
      MultiJson.load(response.body)
    end

    def list_currencies
      response = request.make(:get, 'currencies')
      MultiJson.load(response.body)
    end

    def signup(options)
      response = request.make(:post, 'signup', params: options)
      MultiJson.load(response.body)
    end

    def login(options)
      response = request.make(:post, 'login', creds: options)
      MultiJson.load(response.body)
    end

    def logout(options)
      response = request.make(:post, 'logout', token: options[:token])
      MultiJson.load(response.body)
    end

    def forgot(options)
      params = {organization_name: options[:organization_name], user_email: options[:user_email]}
      response = request.make(:post, 'forgot_password', params: params)
      MultiJson.load(response.body)
    end

    def verify_password(options)
      params = {user_email: options[:user_email],
                organization_name: options[:organization_name]}
      response = request.make(:get, "verify_password/#{options[:token]}", params)
      MultiJson.load(response.body)
    end

    def update_password(options)
      response = request.make(:post, 'update_password', params: options)
      MultiJson.load(response.body)
    end

    def verify_invite(options)
      params = {user_email: options[:user_email],
                organization_name: options[:organization_name]}
      response = request.make(:get, "verify_invite/#{options[:token]}", params)
      MultiJson.load(response.body)
    end

    def invite(options)
      params = {user_email: options[:user_email],
                organization_name: options[:organization_name]}

      response = request.make(:post, 'admin/invite', params, options[:token])
      MultiJson.load(response.body)
    end

    def member_signup(options)
      response = request.make(:post, 'member_signup', params: options)
      MultiJson.load(response.body)
    end

    def users(options)
      params = {organization_name: options[:organization_name]}
      response = request.make(:get, 'admin/users', params, options[:token])
      MultiJson.load(response.body)
    end

    def categories(options)
      params = {organization_name: options[:organization_name]}
      response = request.make(:get, 'organizations/categories', params, options[:token])
      MultiJson.load(response.body)
    end

    def create_category(options)
      params = {organization_name: options[:organization_name], name: options[:name]}
      response = request.make(:post, 'organizations/categories', params, options[:token])
      MultiJson.load(response.body)
    end

    def update_category(options)
      params = {organization_name: options[:organization_name], name: options[:name]}
      response = request.make(:put, "organizations/categories/#{options[:id]}", params, options[:token])
      MultiJson.load(response.body)
    end

    def delete_category(options)
      params = {organization_name: options[:organization_name]}
      response = request.make(:delete, "organizations/categories/#{options[:id]}", params, options[:token])
      response.status == 204
    end

    def dashboard_report(options)
      params = {organization_name: options[:organization_name], from: Date.parse(options[:from]).iso8601, to: Date.parse(options[:to]).iso8601}
      response = request.make(:get, 'reports/dashboard/all', params, options[:token])
      MultiJson.load(response.body)
    end

    def export(options)
      response = request.make(:get, 'invoices/export', options, options[:token])
      MultiJson.load(response.body)
    end
  end
end
