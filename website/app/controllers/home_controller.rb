class HomeController < ApplicationController
  before_filter :set_organization, :login_not_required
  skip_before_filter :login_not_required, :only => [:logout]

  def login
    if request.subdomain == 'www'
      render 'home/signin'
    else
      render 'home/login'
    end
  end

  def new_signup
    render 'home/signup'
  end

  def auth
    begin
      auth_params = login_params.merge(team_name: @organization_name)
      set_current_user_token(KuluService::API.new.login(auth_params)['token']) unless current_user_token
      redirect_to root_url(subdomain: @organization_name), :notice => "Logged into #{@organization_name} successfully"
    rescue HTTPService::ClientError
      flash.alert = 'Wrong username or password'
      redirect_to team_signin_url(subdomain: @organization_name)
    rescue HTTPService::Error
      flash.alert = 'There was an error connecting to the server. Please try again.'
      redirect_to team_signin_url(subdomain: @organization_name)
    end

  end

  def team_signin
    if @organization_name.present?
      redirect_to root_url(subdomain: @organization_name) + 'login'
    else
      render 'home/signin'
    end
  end

  def signin
    redirect_to root_url(subdomain: login_params[:team_name]) + 'login'
  end

  def signup
    begin
      KuluService::API.new.signup(signup_params)
      redirect_to root_url(subdomain: signup_params[:organization_name])
    rescue HTTPService::Error
      redirect_to root_url
    end
  end

  def logout
    KuluService::API.new.logout(token: current_user_token)
    session[:current_user_token] = nil if current_user_token
    redirect_to root_url(subdomain: 'www')
  end

  def forgot_password
    render 'home/forgot_password'
  end

  def forgot
    @user_email = forgot_password_params[:user_email]
    KuluService::API.new.forgot(forgot_password_params)
    render 'home/sent_reset_mail'
  end

  def verify_password
    @token = params[:token]
    @user_email = params[:user_email]
    KuluService::API.new.verify_password(verify_password_params.merge(organization_name: @organization_name))
    render 'home/update_password'
  end

  def verify_invite
    @token = params[:token]
    @user_email = params[:user_email]
    KuluService::API.new.verify_invite(verify_invite_params.merge(organization_name: @organization_name))
    render 'home/new_user'
  end

  def new_user
  end

  def member_signup
    begin
      KuluService::API.new.member_signup(member_signup_params)
      flash.notice = "Your user (#{member_signup_params[:user_email]}) was sucessfully created"
    rescue HTTPService::Error
      flash.alert  = 'There was an error creating the user.'
    end

    team_signin
  end

  def update_password
    begin
      KuluService::API.new.update_password(update_password_params)
      flash.notice = "Password reset for (#{update_password_params[:user_email]})"
    rescue HTTPService::Error
      flash.alert  = 'Could not reset the password'
    end

    team_signin
  end

  private

  def set_organization
    @organization_name = request.subdomain if Subdomain.matches?(request)
  end

  def login_not_required
    redirect_to root_url if logged_in? # halts request cycle
  end

  def login_params
    params.permit(:team_name, :user_email, :password)
  end

  def signup_params
    params.permit(:organization_name, :user_email, :user_name, :password, :confirm)
  end

  def forgot_password_params
    params.permit(:organization_name, :user_email)
  end

  def verify_password_params
    params.permit(:token, :user_email, :organization_name)
  end

  def verify_invite_params
    params.permit(:token, :user_email, :organization_name)
  end

  def update_password_params
    params.permit(:password, :confirm, :user_email, :token, :organization_name)
  end

  def member_signup_params
    params.permit(:password, :confirm, :user_email, :user_name, :organization_name, :token)
  end
end
