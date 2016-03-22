require 'http_service/error'

class TranscriberController < ActionController::Base
  protect_from_forgery with: :exception

  rescue_from(HTTPService::ClientError) do |exception|
    raise ActionController::RoutingError.new('Not Found') if exception.http_status == 404
  end

  before_filter :require_transcriber_login
  skip_before_filter :require_transcriber_login, :only => [:login, :authorize]
  layout 'transcriber'

  include ApplicationHelper

  helper_method :sort_column, :sort_direction

  def index
    @invoices = PaginatedInvoices.new(KuluService::TranscriberAPI.new.list_invoices(index_params.merge!(token: current_transcriber_user_token,
                                                                                                      user_email: current_transcriber_user_email)))

  end

  def show
    begin
      @direction = params[:direction]
      @order = params[:order]
      @invoice = Invoice.new(KuluService::TranscriberAPI.new.find_invoice(id: params[:id],
                                                                        token: current_transcriber_user_token,
                                                                        user_email: current_transcriber_user_email)).decorate
      raw_data = KuluService::TranscriberAPI.new.next_and_prev_invoices(show_params.merge(token: current_transcriber_user_token,
                                                                                        user_email: current_transcriber_user_email))
      @currencies = Currency.all
      @invoices = Invoices.new(next_invoice: raw_data['next_item_id'],
                              prev_invoice: raw_data['prev_item_id'],
                              current_invoice: raw_data['id'])
    rescue HTTPService::Error
      logout
    end
  end

  def login
    redirect_to transcriber_index_path if logged_in?
  end

  def update
    date = params[:invoice][:date]
    params[:invoice].merge!(date: Date.parse(date).iso8601) if date
    params.merge!(token: current_transcriber_user_token, user_email: current_transcriber_user_email)
    @invoice = Invoice.new(KuluService::TranscriberAPI.new.update_invoice(params))
    render json: { invoice: @invoice }
  end

  def authorize
    begin
      creds = KuluService::TranscriberAPI.new.login(login_params)
      set_current_transcriber_user(creds['token'], login_params['user_email'])
      redirect_to transcriber_index_path
    rescue HTTPService::Error
      redirect_to login_transcriber_index_path
    end
  end

  private

  def require_transcriber_login
    redirect_to login_transcriber_index_path unless logged_in? # halts request cycle
  end

  def sort_column
    %w(name amount currency remarks date expense_type status conflict).include?((params[:order] || '').downcase) ? params[:order] : 'created_at'
  end

  def sort_direction
    %w[asc desc].include?(params[:direction]) ? params[:direction] : 'desc'
  end

  def logged_in?
    current_transcriber_user_token.present? && current_transcriber_user_email.present?
  end

  def set_current_transcriber_user(token, email)
    session[:current_transcriber_user_token] = token
    session[:current_transcriber_user_email] = email
  end

  def current_transcriber_user_token
    @current_transcriber_user_token ||= session[:current_transcriber_user_token]
  end

  def current_transcriber_user_email
    @current_transcriber_user_email ||= session[:current_transcriber_user_email]
  end

  def show_params
    params.permit(:id, :direction, :order)
  end

  def index_params
    params.permit(:order, :direction, :per_page, :page)
  end

  def login_params
    params.permit(:user_email, :password)
  end
end

