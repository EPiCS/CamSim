
%calculates normfactor --> the based on the maximum of RES
% RES holds a matrix of at least 3 columns. the factor is caluclated on 1
% and 3
% returns an array with 2 values, first one is the normfactor for column 1
% and the second value is the normfactor for the 3 column.
function normFactors = CalcNormFactors(RES)
    if numel(RES) > 0
        xbar_max = RES(:,1) + RES(:,2);
        ybar_max = RES(:,3) + RES(:,4);

        normFactorX = norm(RES(:,1), inf);
        normFactorY = norm(RES(:,3), inf);

        normFactors = [normFactorX normFactorY];
    else
        normFactors = [1.0,1.0];
    end
end