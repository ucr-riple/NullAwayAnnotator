import plotly
import plotly.graph_objs as go

rounds = []
total = []
fixable = []
processed = []


info1 = [
    [1, 933, 513, 511],
    [2, 891, 336, 334],
    [3, 1230, 324, 322],
    [4, 1185, 233, 231],
    [5, 1336, 146, 140],
    [6, 1357, 103, 97],
    [7, 1433, 68, 62],
    [8, 1482, 74, 68],
    [9, 1549, 76, 70],
    [10, 1557, 55, 49],
    [11, 1545, 31, 25],
    [12, 1536, 17, 11],
    [13, 1526, 6, 0],
]

info = [
    [1, 933, 655, 653]
    , [2, 1083, 389, 387]
    , [3, 1514, 385, 383]
    , [4, 1643, 298, 296]
    , [5, 1703, 173, 167]
    , [6, 1769, 123, 117]
    , [7, 1857, 80, 74]
    , [8, 1914, 93, 87]
    , [9, 1975, 84, 78]
    , [10, 1944, 39, 33]
    , [11, 1949, 27, 21]
    , [12, 1929, 10, 4]
    , [13, 1927, 7, 1]
    , [14, 1927, 6, 0]
]

for round_info in info:
    rounds.append(round_info[0])
    total.append(round_info[1])
    fixable.append(round_info[2])
    processed.append(round_info[3])

# Create traces
trace0 = go.Line(
    x=rounds,
    y=processed,
    mode='lines+markers',
    name='Processed Errors'
)

trace1 = go.Line(
    x=rounds,
    y=total,
    mode='lines+markers',
    name='Total Errors by NullAway'
)

layout = dict(title='Number of Errors VS. Iteration',
              xaxis=dict(title='Iteration Number'),
              yaxis=dict(title='Number of Errors'),
              )

data = [trace0, trace1]
fig = dict(data=data, layout=layout)
plotly.offline.plot(fig, filename='Errors_Iteration_v1')
