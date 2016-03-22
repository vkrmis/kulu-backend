require 'rails_helper'

RSpec.describe Invoice, :type => :model do
  let(:invoice_results) {
    {
      'items'=>
      [
       {
         'id' => 'df56565f-7a24-4701-9ac9-f29235a1f00e',
         'name' => 'Invoice #1',
         'date' => '2014-08-08T18:30:00.000Z',
         'currency' => 'INR',
         'amount' => 20000,
         'remarks' => "FOO",
         'expense_type' => "Company"
       },
       {
         'id' => '4aa80645-ec43-4b57-8df0-aacdef8efc67',
         'name' => 'Invoice #2',
         'date' => '2014-08-18T22:44:00.000Z',
         'currency' => 'USD',
         'amount' => 201.22,
         'remarks' => "HELLO",
         'expense_type' => "Personal"
       }
      ],
      'meta' => {
        'page' => 2,
        'per_page' => 1,
        'total_count' => 10,
        'total_pages' => 10
      }
    }
  }

  let(:invoice_result) {
    {
      'id' => 'df56565f-7a24-4701-9ac9-f29235a1f00e',
      'name' => 'Invoice #1',
      'date' => '2014-08-08T18:30:00.000Z',
      'currency' => 'INR',
      'amount' => 20000,
      'remarks' => "HELLO",
      'expense_type' => "Personal"
     }
  }

  context 'new' do
    it 'builds the storage_key' do
      invoice = Invoice.new(url_prefix: '/foo/bar/${filename}',
                            filename: 'filename.png')
      expect(invoice.storage_key).to eq('/foo/bar/filename.png')
    end
  end

  context 'create' do
    it 'should populate errors if the service calls fails' do
      allow_any_instance_of(KuluService::API).to receive(:create_invoice) { raise HTTPService::Error.new(200, '') }
      invoice = Invoice.create('/foo/bar/${filename}', 'filename.png')
      expect(invoice.errors).to_not be_empty
    end

    it 'builds a new Invoice object' do
      allow_any_instance_of(KuluService::API).to receive(:create_invoice)
      expect(Invoice.create('/foo/bar/${filename}', 'filename.png')).to be_a_kind_of(Invoice)
    end
  end

  context 'list' do
    it 'lists invoice objects returned from the service' do
      expect_any_instance_of(KuluService::API).to receive(:list_invoices).and_return(invoice_results)
      invoices = Invoice.list
      expect(invoices.map(&:name)).to match_array(['Invoice #2'])
    end
  end

  context 'find' do
    it 'fetches the invoice matching the id' do
      expect_any_instance_of(KuluService::API).to receive(:find_invoice).and_return(invoice_result)
      invoice = Invoice.find('df56565f-7a24-4701-9ac9-f29235a1f00e')
      expect(invoice.name).to eq('Invoice #1')
      expect(Date.parse(invoice.date)).to eq(Date.new(2014, 8, 8))
      expect(invoice.currency).to eq('INR')
      expect(invoice.amount).to eq(20000)
      expect(invoice.remarks).to eq("HELLO")
      expect(invoice.expense_type).to eq("Personal")
    end
  end
end
