class AdminController < ApplicationController
  before_filter :set_organization, :require_login

  def index
    render 'admin/index'
  end

  def invite
    begin
      KuluService::API.new.invite(api_params(invite_params))
      flash.notice = "Sent an invite to #{invite_params[:user_email]}"
      render :nothing => true
    rescue HTTPService::ClientError => e
      flash.alert = "#{invite_params[:user_email]} is already a member"
      render json: e.to_json, status: 400 and return
    end
  end


  def users
    begin
      render json: KuluService::API.new.users(api_params({})).to_json
    rescue HTTPService::ClientError => e
      flash.alert = "#{e}"
      render json: e.to_json, status: 400 and return
    end
  end

  def categories
    begin
      render json: KuluService::API.new.categories(categories_params).to_json
    rescue HTTPService::ClientError => e
      flash.alert = "#{e}"
      render json: e.to_json, status: 400 and return
    end
  end

  def create_category
    begin
      render json: KuluService::API.new.create_category(api_params(category_params)).to_json
    rescue HTTPService::ClientError => e
      flash.alert = "#{e}"
      render json: e.to_json, status: 400 and return
    end
  end

  def update_category
    begin
      render json: KuluService::API.new.update_category(api_params(category_params)).to_json
    rescue HTTPService::ClientError => e
      flash.alert = "#{e}"
      render json: e.to_json, status: 400 and return
    end
  end

  def delete_category
    begin
      render json: KuluService::API.new.delete_category(api_params(delete_category_params)).to_json
    rescue HTTPService::ClientError => e
      flash.alert = "#{e}"
      render json: e.to_json, status: 400 and return
    end
  end

  private

  def set_organization
    @organization_name = request.subdomain if Subdomain.matches?(request)
  end

  def api_params(base_params)
    base_params.merge(organization_name: @organization_name, token: current_user_token)
  end

  def require_login
    unless logged_in?
      flash[:error] = 'You must be logged in to access this section'
      redirect_to root_url # halts request cycle
    end
  end

  def invite_params
    params.permit(:user_email)
  end

  def categories_params
    params.permit(:token, :organization_name)
  end

  def category_params
    params.permit(:id, :name)
  end

  def delete_category_params
    params.permit(:id)
  end
end
