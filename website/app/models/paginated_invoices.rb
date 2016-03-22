class PaginatedInvoices < Kaminari::PaginatableArray
  PAGINATION_KEYS = %w{page per_page total_pages total_count}

  attr_reader :current_page, :total_pages, :limit_value

  def initialize(raw_data)
    invoices = raw_data['items'].map {|i| Invoice.new(i).decorate }.reverse
    @current_page, @limit_value, @total_pages, @total_count = raw_data['meta'].values_at(*PAGINATION_KEYS)
    super(invoices, total_count: @total_count, limit: limit_value)
  end

  def offset_value
    (current_page - 1) * limit_value
  end

  def last_page?
    current_page == total_pages
  end
end
