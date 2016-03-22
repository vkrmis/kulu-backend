require 'rails_helper'
require 'http_service/error'

RSpec.describe InvoicesController, :type => :controller do
  before(:each) do
    session[:current_user_token] = 'foo'
  end

  context 'POST create' do
    let(:invoice) { Invoice.new.tap { |i| i.id = 'foo' } }

    it 'creates an invoice resource' do
      expect(Invoice).to receive(:create) {invoice}
      post :create, invoice: { filename: 'foo.png', url_prefix: 'foo/bar/${filename}' }
    end

    it 'redirects to the root path' do
      allow(Invoice).to receive(:create) {invoice}
      post :create, invoice: { filename: 'foo.png', url_prefix: 'foo/bar/${filename}' }

      expect(response).to redirect_to(invoice_path(invoice.id))
    end
  end

  context 'GET show' do
    let(:invoice_result) {
      {
        'id' => 'df56565f-7a24-4701-9ac9-f29235a1f00e',
        'name' => 'Invoice #1',
      }
    }

    it 'fetches the invoice' do
      expected_currencies = ["USD", "INR"]
      expect_any_instance_of(KuluService::API).to receive(:list_currencies).and_return(expected_currencies)

      expect_any_instance_of(KuluService::API).to receive(:find_invoice) \
        .with('df56565f-7a24-4701-9ac9-f29235a1f00e').and_return(invoice_result)

      get :show, id: 'df56565f-7a24-4701-9ac9-f29235a1f00e'
      invoice = assigns(:invoice)
      expect(invoice.name).to eq('Invoice #1')
      currencies = assigns(:currencies)
      expect(currencies).to match_array(expected_currencies)
    end

  end

  context 'PUT update' do
    let(:invoice_result) {
      {
          'id' => 'df56565f-7a24-4701-9ac9-f29235a1f00e',
          'name' => 'New Invoice',
          'date' => '2014-08-08T18:30:00.000Z',
          'currency' => 'INR',
          'amount' => 500
      }
    }

    it 'updates the invoice name, amount, date and currency' do
      expected_params = {name: 'New Invoice', currency: 'INR', amount: '500'}
      expect_any_instance_of(KuluService::API).to receive(:update_invoice).with('df56565f-7a24-4701-9ac9-f29235a1f00e', expected_params).and_return(invoice_result)
      put :update, id: 'df56565f-7a24-4701-9ac9-f29235a1f00e', invoice: expected_params
      invoice = JSON.parse(response.body, symbolize_names: true)[:invoice]
      expect(invoice[:name]).to eq('New Invoice')
      expect(invoice[:amount]).to eq(500)
      expect(invoice[:currency]).to eq('INR')
    end
  end

  context 'DELETE destroy' do
    it 'deletes the invoice record' do
      expect_any_instance_of(KuluService::API).to receive(:delete_invoice).with('df56565f-7a24-4701-9ac9-f29235a1f00e').and_return(true)
      delete :destroy, id: 'df56565f-7a24-4701-9ac9-f29235a1f00e'
      expect(response).to redirect_to(root_path)
      expect(flash[:notice]).to be_present
    end
  end
end
