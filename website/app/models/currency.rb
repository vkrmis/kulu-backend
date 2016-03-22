class Currency
  class << self
    def all
      KuluService::API.new.list_currencies
    end
  end
end
