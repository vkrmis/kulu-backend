class InvoiceDecorator < Draper::Decorator
  delegate_all

  def attachment_url
    u = object.attachment_url

    if u.present?
      object.attachment_url.html_safe
    else
      helpers.asset_path('placeholder-portrait-450x600.jpg')
    end
  end

  def name
    object.name || '-'
  end

  def display_date
    formatted_date(object.date, '%d %b %Y')
  end

  def submission_date
    formatted_date(object.created_at, '%d %b %Y')
  end

  def input_date
    formatted_date(object.date, '%d-%m-%Y')
  end

  def conflict?
    conflict
  end

  private

  def formatted_date(d, format)
    if d.blank?
      '-'
    else
      Date.parse(d).strftime(format)
    end
  end
end
