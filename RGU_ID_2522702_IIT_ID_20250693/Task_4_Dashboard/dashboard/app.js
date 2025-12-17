// Weather Dashboard - Coursework Visualization
// Using Chart.js for graphs

document.addEventListener("DOMContentLoaded", function () {
  // load all the charts when page is ready
  initPrecipitationChart();
  initTopDistrictsChart();
  initTemperatureChart();
  initExtremeEventsChart();
});

// precipitation chart - shows monthly patterns
function initPrecipitationChart() {
  const ctx = document.getElementById("precipitationChart").getContext("2d");

  // data from my spark analysis results
  const months = [
    "Jan",
    "Feb",
    "Mar",
    "Apr",
    "May",
    "Jun",
    "Jul",
    "Aug",
    "Sep",
    "Oct",
    "Nov",
    "Dec",
  ];

  // monthly precipitation data for selected districts (in mm)
  const districtData = {
    Ratnapura: [285, 245, 312, 368, 402, 285, 195, 178, 245, 485, 398, 362],
    Kalutara: [198, 165, 228, 298, 342, 245, 168, 152, 212, 398, 342, 298],
    Galle: [178, 145, 212, 285, 328, 268, 185, 168, 228, 342, 387, 325],
    Batticaloa: [285, 245, 198, 125, 98, 72, 85, 112, 145, 268, 342, 425],
  };

  const colors = [
    "rgba(102, 126, 234, 0.8)",
    "rgba(118, 75, 162, 0.8)",
    "rgba(255, 99, 132, 0.8)",
    "rgba(75, 192, 192, 0.8)",
  ];

  const datasets = Object.entries(districtData).map(
    ([district, data], index) => {
      return {
        label: district,
        data: data,
        backgroundColor: colors[index],
        borderColor: colors[index].replace("0.8", "1"),
        borderWidth: 2,
        tension: 0.4,
      };
    }
  );

  new Chart(ctx, {
    type: "line",
    data: {
      labels: months,
      datasets: datasets,
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        title: {
          display: true,
          text: "Monthly Precipitation Patterns (mm)",
          font: { size: 16 },
        },
        legend: {
          position: "top",
        },
        tooltip: {
          callbacks: {
            label: function (context) {
              return context.dataset.label + ": " + context.parsed.y + "mm";
            },
          },
        },
      },
      scales: {
        y: {
          beginAtZero: true,
          title: {
            display: true,
            text: "Precipitation (mm)",
          },
        },
        x: {
          title: {
            display: true,
            text: "Month",
          },
        },
      },
    },
  });
}

// top 5 districts chart
function initTopDistrictsChart() {
  const ctx = document.getElementById("topDistrictsChart").getContext("2d");

  // total precipitation data (mm) from my analysis
  const topDistricts = [
    { name: "Ratnapura", total: 54287, annual: 3747, days: 1847 },
    { name: "Kalutara", total: 47823, annual: 3302, days: 1623 },
    { name: "Galle", total: 45961, annual: 3173, days: 1591 },
    { name: "Matara", total: 44108, annual: 3045, days: 1542 },
    { name: "Batticaloa", total: 38592, annual: 2662, days: 1289 },
  ];

  new Chart(ctx, {
    type: "bar",
    data: {
      labels: topDistricts.map((d) => d.name),
      datasets: [
        {
          label: "Total Precipitation (mm)",
          data: topDistricts.map((d) => d.total),
          backgroundColor: [
            "rgba(255, 215, 0, 0.8)",
            "rgba(192, 192, 192, 0.8)",
            "rgba(205, 127, 50, 0.8)",
            "rgba(102, 126, 234, 0.8)",
            "rgba(75, 192, 192, 0.8)",
          ],
          borderColor: [
            "rgba(255, 215, 0, 1)",
            "rgba(192, 192, 192, 1)",
            "rgba(205, 127, 50, 1)",
            "rgba(102, 126, 234, 1)",
            "rgba(75, 192, 192, 1)",
          ],
          borderWidth: 2,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        title: {
          display: true,
          text: "Top 5 Districts - Total Precipitation",
          font: { size: 16 },
        },
        legend: {
          display: false,
        },
        tooltip: {
          callbacks: {
            label: function (context) {
              const district = topDistricts[context.dataIndex];
              return [
                "Total: " + formatNumber(district.total) + " mm",
                "Annual Avg: " + formatNumber(district.annual) + " mm",
                "Rainy Days: " + formatNumber(district.days),
              ];
            },
          },
        },
      },
      scales: {
        y: {
          beginAtZero: true,
          title: {
            display: true,
            text: "Total Precipitation (mm)",
          },
          ticks: {
            callback: function (value) {
              return formatNumber(value) + " mm";
            },
          },
        },
        x: {
          title: {
            display: true,
            text: "District",
          },
        },
      },
    },
  });
}

// temperature chart - monthly percentages
function initTemperatureChart() {
  const ctx = document.getElementById("temperatureChart").getContext("2d");

  // percentage of months per year where mean temp was above 30C
  const years = [
    2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020, 2021,
    2022, 2023, 2024,
  ];
  const percentages = [
    25.0, 33.3, 41.7, 33.3, 41.7, 50.0, 58.3, 50.0, 41.7, 50.0, 58.3, 50.0,
    58.3, 58.3, 50.0,
  ];

  new Chart(ctx, {
    type: "line",
    data: {
      labels: years,
      datasets: [
        {
          label: "% of Months with Mean Temperature > 30°C",
          data: percentages,
          borderColor: "rgba(245, 87, 108, 1)",
          backgroundColor: "rgba(245, 87, 108, 0.2)",
          borderWidth: 3,
          fill: true,
          tension: 0.4,
          pointRadius: 6,
          pointHoverRadius: 9,
          pointBackgroundColor: "rgba(245, 87, 108, 1)",
          pointBorderColor: "#fff",
          pointBorderWidth: 2,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        title: {
          display: true,
          text: "Hot Months Analysis (Mean Temp > 30°C)",
          font: { size: 16 },
        },
        legend: {
          position: "top",
        },
        tooltip: {
          callbacks: {
            label: function (context) {
              const year = context.label;
              const pct = context.parsed.y;
              const months = Math.round((pct / 100) * 12);
              return [
                context.dataset.label + ": " + pct + "%",
                "(" + months + " out of 12 months)",
              ];
            },
          },
        },
      },
      scales: {
        y: {
          beginAtZero: true,
          max: 70,
          title: {
            display: true,
            text: "Percentage of Months (%)",
          },
          ticks: {
            callback: function (value) {
              return value + "%";
            },
          },
        },
        x: {
          title: {
            display: true,
            text: "Year",
          },
        },
      },
    },
  });
}

// extreme weather events - combined high rain and wind
function initExtremeEventsChart() {
  const ctx = document.getElementById("extremeEventsChart").getContext("2d");

  // days with both heavy rain (>=50mm) AND strong wind (>=60 km/h)
  const years = [
    2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020, 2021,
    2022, 2023, 2024,
  ];
  const extremeDays = [
    89, 98, 112, 108, 121, 128, 135, 142, 118, 138, 151, 145, 163, 183, 116,
  ];

  // Calculate trend line
  const trend = calculateTrend(extremeDays);
  const trendData = extremeDays.map((_, i) => {
    return extremeDays[0] + trend * i;
  });

  new Chart(ctx, {
    type: "bar",
    data: {
      labels: years,
      datasets: [
        {
          label: "Extreme Days (Precip ≥50mm AND Wind ≥60km/h)",
          data: extremeDays,
          backgroundColor: "rgba(255, 107, 107, 0.8)",
          borderColor: "rgba(255, 107, 107, 1)",
          borderWidth: 2,
        },
        {
          label: "Trend Line",
          data: trendData,
          type: "line",
          borderColor: "rgba(30, 60, 114, 1)",
          backgroundColor: "rgba(30, 60, 114, 0.1)",
          borderWidth: 3,
          fill: false,
          tension: 0,
          pointRadius: 0,
          borderDash: [5, 5],
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        title: {
          display: true,
          text: "Extreme Weather Days (Rain + Wind)",
          font: { size: 16 },
        },
        legend: {
          position: "top",
        },
        tooltip: {
          callbacks: {
            label: function (context) {
              if (context.datasetIndex === 0) {
                return [
                  "Extreme Days: " + context.parsed.y,
                  "Criteria: ≥50mm rain AND ≥60km/h wind",
                ];
              }
              return "Trend: " + Math.round(context.parsed.y);
            },
          },
        },
      },
      scales: {
        y: {
          beginAtZero: true,
          title: {
            display: true,
            text: "Number of Days",
          },
        },
        x: {
          title: {
            display: true,
            text: "Year",
          },
        },
      },
    },
  });
}

// helper functions
function formatNumber(num) {
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

function calculateAverage(arr) {
  return arr.reduce((a, b) => a + b, 0) / arr.length;
}

function calculateTrend(arr) {
  const n = arr.length;
  const avgX = (n - 1) / 2;
  const avgY = calculateAverage(arr);

  let numerator = 0;
  let denominator = 0;

  for (let i = 0; i < n; i++) {
    numerator += (i - avgX) * (arr[i] - avgY);
    denominator += Math.pow(i - avgX, 2);
  }

  return numerator / denominator;
}
