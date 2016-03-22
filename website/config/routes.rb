Rails.application.routes.draw do
  constraints(Subdomain) do
    concern :paginatable do
      get '(page/:page)', :action => :index, :on => :collection, :as => ''
    end

    resources :expenses, controller: 'invoices', :concerns => :paginatable, :as => 'invoices' do
      collection do
        post :fetch_attachment
        get  :dashboard
        get  :search
        get  :export
      end
    end
  end

  resources :transcriber, controller: 'transcriber', :as => 'transcriber' do
    collection do
      get  :login
      post :authorize
    end
  end

  post '/login',   to: 'home#auth'
  get  '/login',   to: 'home#login'
  post '/signin',  to: 'home#signin'
  get  '/signin',  to: 'home#team_signin', as: :team_signin
  get  '/signup',  to: 'home#new_signup'

  get  '/forgot_password',  to: 'home#forgot_password', as: :forgot_password
  post '/forgot',  to: 'home#forgot'

  get  '/verify_password/:token',  to: 'home#verify_password'
  post '/update_password',  to: 'home#update_password'

  get  '/logout',  to: 'home#logout'
  post '/signup',  to: 'home#signup'

  get  '/admin', to: 'admin#index'
  get  '/users', to: 'admin#users'

  get     '/categories', to: 'admin#categories'
  post    '/categories', to: 'admin#create_category'
  put     '/categories/:id', to: 'admin#update_category'
  delete  '/categories/:id', to: 'admin#delete_category'

  post '/invite', to: 'admin#invite'
  post '/member_signup', to: 'home#member_signup'
  get  '/verify_invite/:token', to: 'home#verify_invite'

  root 'invoices#dashboard'
end
