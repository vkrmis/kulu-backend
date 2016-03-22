class Report
  include ActiveModel::Model

  attr_reader :currency_breakdown, :reviewed, :submitted, :awaiting_review, :awaiting_approval, :category_breakdown

  def initialize(opts)
    @reports = KuluService::API.new.dashboard_report(opts)['report']
    @currency_breakdown = @reports['currency_breakdown']
    @reviewed = @reports['reviewed']
    @submitted = @reports['submitted']
    @conflicted = @reports['conflicted']
    @awaiting_review = @reports['awaiting_review']
    @awaiting_approval = @reports['awaiting_approval']
    @category_breakdown = @reports['category_breakdown']
  end

  def total(which)
    @reports[which].inject(0) do |acc, i|
      acc += i['count'].to_i
      acc
    end
  end

  def category_breakdown
    @category_breakdown.inject([]) { |a, i| a << {i['name'] || 'Uncategorized' => i} }
        .flat_map(&:entries)
        .group_by(&:first)
        .map { |k, v| Hash[k, v.map(&:last)] }
        .inject({}) { |a, i| a.merge!(i) }
  end
end
