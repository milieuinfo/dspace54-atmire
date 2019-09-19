(function ($) {

    $(document).ready(function() {

        $('.subdivision').change(function () {
            var value = $(this).val();
            $('.helpText').addClass("hidden");
            $('#aspect_submission_StepTransformer_p_' + value).removeClass("hidden");
        });
        $('input[name="exception-options"]').click(function () {
            var value = $(this).val();
            $('.specificExceptions').addClass("hidden");
            $("#aspect_submission_StepTransformer_div_" + value).removeClass("hidden");
            $('.helpText').addClass("hidden");
            var helpText = $('select[name="' + value + '-dropdown"] option:selected').val();
            $('#aspect_submission_StepTransformer_p_' + helpText).removeClass("hidden");

        });

    });

})(jQuery);
