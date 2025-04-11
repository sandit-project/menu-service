$(document).ready(function () {
    // checkToken();
    // setupAjax();

    $("#submitBtn").on("click", function () {
        let fileInput = $("#img")[0].files[0];

        if (!fileInput) {
            alert("이미지를 업로드해야 합니다.");
            return;
        }

        let formData = new FormData();
        formData.append("file", fileInput); // 이미지 파일 추가

        let materialData = {
            materialName: $("#materialName").val(),
            calorie: parseFloat($("#calorie").val()),
            price: parseInt($("#price").val(), 10),
            status: $("#status").val() === "active" ? "ACTIVE" : "DELETED"
        };

        // ✅ JSON 데이터를 Blob으로 변환하여 FormData에 추가
        let jsonBlob = new Blob([JSON.stringify(materialData)], { type: "application/json" });
        formData.append("material", jsonBlob); // 백엔드에서 @RequestPart("material")와 일치

        $.ajax({
            url: "/materials",
            type: "POST",
            data: formData,
            enctype: "multipart/form-data",
            processData: false,
            contentType: false,
            success: function () {
                alert("재료 정보가 등록되었습니다!");
                window.location.href = "/materials/list"; // 필요 없으면 주석 처리해도 됨
            },
            error: function (xhr) {
                console.error("Error:", xhr.responseText);
                alert("재료 정보 등록 중 오류가 발생했습니다.");
            }
        });
    });
});
