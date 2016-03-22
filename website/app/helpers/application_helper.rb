module ApplicationHelper
  def sortable(column, title = nil)
    new_column = convert(column)
    title ||= column.titleize
    css_class = (new_column == sort_column) ? "current #{sort_direction}" : nil
    direction = (new_column == sort_column && sort_direction == 'asc') ? 'desc' : 'asc'

    sort_params = {:order => new_column, :direction => direction}

    if @params
      link_params = @params.merge(sort_params)
    else
      link_params = sort_params
    end

    link_to title, link_params, {:class => css_class}
  end

  def convert(column)
    {
        'Merchant' => 'name',
        'Expense Date' => 'date',
        'Submission Date' => 'created_at',
        'Amount' => 'amount',
        'Remarks' => 'remarks',
        'Type' => 'expense_type',
        'Status' => 'status',
        'Conflict' => 'conflict',
        'Spender' => 'user_name'
    }[column]
  end

  def sorted_params
    "direction=#{sorted_direction}&order=#{sorted_order.downcase}"
  end

  private

  def sorted_direction
    !params[:direction].blank? ? params[:direction] : 'desc'
  end

  def sorted_order
    !params[:order].blank? ? params[:order] : 'created_at'
  end

  def amount_with_currency(amount, currency)
    return '-' if amount.blank? or currency.blank?
    c = Money::Currency.new(currency)
    money = Money.new(amount * c.subunit_to_unit, c).format(:with_currency => false, :no_cents_if_whole => true)
    "#{currency} #{money}"
  end
end
