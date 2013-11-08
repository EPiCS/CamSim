function [Mean, Std] = CalculateStatsPerRun(table)
    Mean= mean(table).';
    Std = std(table).';
end