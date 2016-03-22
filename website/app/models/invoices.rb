class Invoices
  include ActiveModel::Model

  attr_accessor :next_invoice, :prev_invoice, :current_invoice

  class << self
    def next_and_prev_invoices(options)
      raw_data = KuluService::API.new.next_and_prev_invoices(options)
      new(next_invoice: raw_data['next_item_id'],
          prev_invoice: raw_data['prev_item_id'],
          current_invoice: raw_data['id'])
    end
  end
end
