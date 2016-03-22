Kulu.Reports = new function() {
  function generateReport(params) {
    $.ajax({
      url: "/expenses/export",
      data: params,
      success: function (data) {
        $('nav').append("<div class='flash-message'><p>" + data.message + "</p></div>");
      },
      failure: function (error) {
	$('nav')
	  .append("<div class='flash-message'><p>" + error + "</p></div>");
      }
    });
  }

  return {
    generateReport: generateReport
   };

};
